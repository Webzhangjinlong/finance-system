package com.finance.finance;

import com.finance.finance.domain.FinSubject;
import com.finance.finance.service.SubjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.finance.common.core.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 科目管理集成测试（F1，真实 PG 测试库 + Flyway 种子数据，事务回滚）。
 *
 * <p>验证硬约束：编码唯一（DB 唯一约束兜底）、删除保护（子科目/发生额）、树形结构。
 * 种子：DEMO 公司 29 个一级科目（1001 库存现金…6711 营业外支出）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubjectServiceTest {

    @Autowired
    private SubjectService subjectService;

    @Test
    void tree_returnsAllSeedRootSubjects() {
        List<FinSubject> tree = subjectService.tree("DEMO");
        assertThat(tree).hasSize(29);
        assertThat(tree).allMatch(s -> s.getParentId() == 0L);
        assertThat(tree).allMatch(s -> s.getSubjectLevel() == 1);
        // 按编码升序
        assertThat(tree.get(0).getSubjectCode()).isEqualTo("1001");
        assertThat(tree.get(tree.size() - 1).getSubjectCode()).isEqualTo("6711");
    }

    @Test
    void addSecondLevelSubject_parentBecomesNonLeaf() {
        FinSubject child = newSubject("100201", "银行存款-工行", "ASSET", "DEBIT", 2002L);
        subjectService.add("DEMO", child);

        FinSubject created = findById(child.getId());
        assertThat(created.getSubjectLevel()).isEqualTo(2);
        assertThat(created.getIsLeaf()).isEqualTo(1);

        FinSubject parent = subjectService.tree("DEMO").stream()
                .filter(s -> s.getSubjectCode().equals("1002"))
                .findFirst().orElseThrow();
        assertThat(parent.getIsLeaf()).isZero();
        assertThat(parent.getChildren()).hasSize(1);
        assertThat(parent.getChildren().get(0).getSubjectCode()).isEqualTo("100201");
    }

    @Test
    void addDuplicateCode_rejected() {
        FinSubject dup = newSubject("1001", "重复现金", "ASSET", "DEBIT", 0L);
        assertThatThrownBy(() -> subjectService.add("DEMO", dup))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void addNonNumericCode_rejected() {
        FinSubject bad = newSubject("ABC", "非法编码", "ASSET", "DEBIT", 0L);
        assertThatThrownBy(() -> subjectService.add("DEMO", bad))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("纯数字");
    }

    @Test
    void deleteWithChildren_rejected() {
        FinSubject child = newSubject("100202", "银行存款-建行", "ASSET", "DEBIT", 2002L);
        subjectService.add("DEMO", child);

        assertThatThrownBy(() -> subjectService.delete("DEMO", 2002L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("子科目");
    }

    @Test
    void deleteWithVoucherEntries_rejected() {
        // 种子示例凭证（BOOKED）分录引用了 1002 银行存款 —— 已有发生额禁止删除
        assertThatThrownBy(() -> subjectService.delete("DEMO", 2002L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发生额");
    }

    @Test
    void deleteLeafWithoutEntries_succeeds() {
        FinSubject leaf = newSubject("100299", "临时科目", "ASSET", "DEBIT", 2002L);
        subjectService.add("DEMO", leaf);
        subjectService.delete("DEMO", leaf.getId());

        assertThat(findById(leaf.getId())).isNull();
    }

    @Test
    void updateCode_rejected() {
        FinSubject update = new FinSubject();
        update.setSubjectCode("9999");
        assertThatThrownBy(() -> subjectService.update("DEMO", 2001L, update))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("编码禁止修改");
    }

    @Test
    void updateName_succeeds() {
        FinSubject update = new FinSubject();
        update.setSubjectName("库存现金-改名");
        subjectService.update("DEMO", 2001L, update);
        assertThat(findById(2001L).getSubjectName()).isEqualTo("库存现金-改名");
    }

    @Test
    void updateTypeWithEntries_rejected() {
        // 2002 银行存款已有发生额（示例凭证），禁改类别
        FinSubject update = new FinSubject();
        update.setSubjectType("LIABILITY");
        assertThatThrownBy(() -> subjectService.update("DEMO", 2002L, update))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("类别");
    }

    private FinSubject newSubject(String code, String name, String type, String direction, Long parentId) {
        FinSubject s = new FinSubject();
        s.setSubjectCode(code);
        s.setSubjectName(name);
        s.setSubjectType(type);
        s.setDirection(direction);
        s.setParentId(parentId);
        return s;
    }

    private FinSubject findById(Long id) {
        return subjectService.tree("DEMO").stream()
                .flatMap(s -> flatten(s).stream())
                .filter(s -> s.getId().equals(id))
                .findFirst().orElse(null);
    }

    private List<FinSubject> flatten(FinSubject node) {
        List<FinSubject> result = new java.util.ArrayList<>();
        result.add(node);
        if (node.getChildren() != null) {
            for (FinSubject child : node.getChildren()) {
                result.addAll(flatten(child));
            }
        }
        return result;
    }
}

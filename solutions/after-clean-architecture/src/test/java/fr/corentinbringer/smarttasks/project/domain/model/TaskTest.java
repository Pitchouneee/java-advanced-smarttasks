package fr.corentinbringer.smarttasks.project.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test du DOMAINE : pas de mock, pas de Spring, juste des objets Java.
 * C'est le test le plus simple et le plus rapide qui soit : le bénéfice direct
 * d'avoir mis les règles métier dans le domaine plutôt que dans le service ou en SQL.
 */
class TaskTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);

    private static Task taskDueOn(LocalDate dueDate) {
        return Task.create("alice", 1L, "Rendre le TP", null, dueDate, Instant.now());
    }

    @Test
    void aTaskWhoseDueDateIsPastIsOverdue() {
        assertThat(taskDueOn(TODAY.minusDays(1)).isOverdue(TODAY)).isTrue();
    }

    @Test
    void aTaskDueTodayIsNotOverdueYet() {
        assertThat(taskDueOn(TODAY).isOverdue(TODAY)).isFalse();
    }

    @Test
    void aTaskWithoutDueDateIsNeverOverdue() {
        assertThat(taskDueOn(null).isOverdue(TODAY)).isFalse();
    }

    @Test
    void aCompletedTaskIsNeverOverdue() {
        Task late = taskDueOn(TODAY.minusDays(10));

        Task done = late.complete();

        assertThat(done.isOverdue(TODAY)).isFalse();
        assertThat(late.completed()).isFalse(); // immuable : l'original n'a pas changé
    }

    @Test
    void aTaskCannotHaveABlankTitle() {
        assertThatThrownBy(() -> Task.create("alice", 1L, "   ", null, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

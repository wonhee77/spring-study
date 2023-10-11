package com.wonhee.springstudy.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class JpaLearningTest {

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("같은 컨텍스트 내에서 같은 id 값을 가지는 객체의 동일성이 보장된다.")
    @Transactional
    void sameReferenceInSameContext() {
        Member member = Member.builder()
            .name("member")
            .age(1)
            .build();

        em.persist(member);

        Member foundMember = em.find(Member.class, member.getId());

        assertThat(member).isEqualTo(foundMember);
    }

    @Test
    @DisplayName("즉시 로딩이고 persist()로 데이터를 조회 시 join으로 team과 member를 가져온다.")
    @Transactional
    void eagerLoading_persist() {
        Team teamA = Team.builder()
            .name("teamA")
            .build();

        em.persist(teamA);

        Member member1 = Member.builder()
            .name("member1")
            .age(1)
            .team(teamA)
            .build();

        em.persist(member1);

        Member member = em.find(Member.class, member1.getId());
    }

    @Test
    @DisplayName("즉시 로딩이고 JPQL로 데이터를 조회하는 경우 N + 1 쿼리가 발생한다.")
    @Transactional
    void lazyLoading_jpql() {
        Team teamA = Team.builder()
            .name("teamA")
            .build();

        Team teamB = Team.builder()
            .name("teamB")
            .build();

        em.persist(teamA);
        em.persist(teamB);

        em.flush();
        em.clear();

        Member member1 = Member.builder()
            .name("member1")
            .age(1)
            .team(teamA)
            .build();

        Member member2 = Member.builder()
            .name("member2")
            .age(1)
            .team(teamB)
            .build();

        em.persist(member1);
        em.persist(member2);

        List<Member> members = em.createQuery("select m from Member m", Member.class).getResultList();
        for (Member member : members) {
            System.out.println("member.getTeam().getName() = " + member.getTeam().getName());
        }
    }
}
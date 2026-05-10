package org.mj.trip.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "MEMBER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(unique = true, nullable = true)
    private String email;

    private String nickname;
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder
    public Member(String email, String nickname, String profileImageUrl, MemberStatus status) {
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.status = status != null ? status : MemberStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }
}
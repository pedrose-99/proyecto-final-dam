package com.smartcart.smartcart.modules.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.common.enums.MemberStatus;
import com.smartcart.smartcart.modules.group.entity.Group;
import com.smartcart.smartcart.modules.group.entity.GroupMember;
import com.smartcart.smartcart.modules.user.entity.User;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Integer> {

    List<GroupMember> findByUserAndStatus(User user, MemberStatus status);

    Optional<GroupMember> findByGroupAndUser(Group group, User user);

    boolean existsByGroupAndUser(Group group, User user);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.user.idUser = :userId AND gm.status = 'ACCEPTED'")
    List<GroupMember> findAcceptedMembershipsByUserId(@Param("userId") Integer userId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.groupId = :groupId AND gm.status = 'ACCEPTED'")
    List<GroupMember> findAcceptedMembersByGroupId(@Param("groupId") Integer groupId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.groupId = :groupId AND gm.user.idUser = :userId AND gm.status = 'ACCEPTED'")
    Optional<GroupMember> findAcceptedMember(@Param("groupId") Integer groupId, @Param("userId") Integer userId);
}

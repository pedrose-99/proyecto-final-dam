package com.smartcart.smartcart.modules.auth.repository;

import com.smartcart.smartcart.modules.auth.entity.Token;
import com.smartcart.smartcart.modules.auth.entity.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer>
{
    Optional<Token> findByToken(String token);

    @Query("SELECT t FROM Token t WHERE t.user.idUser = :userId AND (t.expired = false OR t.revoked = false)")
    List<Token> findAllValidTokensByUser(Integer userId);

    @Query("SELECT t FROM Token t WHERE t.user.idUser = :userId AND t.tokenType = :tokenType AND t.expired = false AND t.revoked = false")
    List<Token> findAllValidTokensByUserAndType(Integer userId, TokenType tokenType);
}

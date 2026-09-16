package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CredentialUserRepository extends JpaRepository<CredentialUserEntity, String> {
}

package com.com4energy.persistence.filerecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseFileRecordRepository extends JpaRepository<FileRecord, Long> {

}

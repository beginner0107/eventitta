package com.eventitta.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {
    @Id
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "parent_code", length = 20)
    private String parentCode;

    private Integer level;
}

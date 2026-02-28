package com.example.superapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TvCreditId implements Serializable {

    @Column(name = "tv_id")
    private Long tvId;

    @Column(name = "person_id")
    private Long personId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TvCreditId)) return false;
        TvCreditId that = (TvCreditId) o;
        return Objects.equals(tvId, that.tvId) &&
            Objects.equals(personId, that.personId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tvId, personId);
    }
}

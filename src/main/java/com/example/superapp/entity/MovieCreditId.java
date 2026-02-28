package com.example.superapp.entity;

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
public class MovieCreditId implements Serializable {

    private Long movieId;
    private Long personId;

    // equals & hashCode bắt buộc
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovieCreditId)) return false;
        MovieCreditId that = (MovieCreditId) o;
        return Objects.equals(movieId, that.movieId) &&
            Objects.equals(personId, that.personId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(movieId, personId);
    }
}

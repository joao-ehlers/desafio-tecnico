package com.irrah.desafio_tecnico.conversation;

import com.irrah.desafio_tecnico.shared.exception.InvalidInputException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "recipients")
public class Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String phone;

    public Recipient(String name, String phone) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("o nome é obrigatório");
        }

        if (phone == null || phone.isBlank()) {
            throw new InvalidInputException("o telefone é obrigatório");
        }

        this.name = name.strip();
        this.phone = phone.strip();
    }
}

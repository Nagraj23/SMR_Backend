package com.spring.smr.entity;

import jakarta.persistence.*;
import lombok.*;
import com.spring.smr.entity.Vehicles;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID user_id;

    private String name ;
    private String email ;
    private String phone;

    private String password ;
    private boolean isVerified;
    private String govIdUrl;

    private String profileStatus;

    @OneToMany(mappedBy = "owner" ,cascade = CascadeType.ALL , fetch = FetchType.LAZY)
    private List<Vehicles> vehicles;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 🚀 Add this field property into your Users.java class file:

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_face_embeddings",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "embedding_value")
    private List<Double> faceEmbedding;

}

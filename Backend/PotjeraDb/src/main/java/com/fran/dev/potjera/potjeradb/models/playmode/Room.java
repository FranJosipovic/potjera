package com.fran.dev.potjera.potjeradb.models.playmode;

import com.fran.dev.potjera.potjeradb.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = true, length = 6)
    private String code;

    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hunter_id", nullable = true)
    private User hunter;*/

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    private int maxPlayers = 5;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "room", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<RoomPlayer> players = new ArrayList<>();

    /*@OneToOne(mappedBy = "room", cascade = CascadeType.ALL)
    private GameSession gameSession;*/
}

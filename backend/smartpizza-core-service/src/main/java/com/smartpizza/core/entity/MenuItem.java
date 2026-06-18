package com.smartpizza.core.entity;

import com.smartpizza.core.enums.CrustType;
import com.smartpizza.core.enums.ItemSize;
import com.smartpizza.core.enums.SpiceLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ItemSize size;

    @Enumerated(EnumType.STRING)
    private CrustType crustType;

    @Enumerated(EnumType.STRING)
    private SpiceLevel spiceLevel;

    private Boolean veg;

    private Boolean available;

    private Double rating;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)   //many menu items can belongs to single category
    @JoinColumn(name = "category_id")
    private Category category;

    @PrePersist  //Before a new menu item is inserted into DB, Hibernate automatically calls onCreate().
    public void onCreate() {
        this.available = true; //by default make the added item available that is true whenever added
        this.rating = this.rating == null ? 4.5 : this.rating;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
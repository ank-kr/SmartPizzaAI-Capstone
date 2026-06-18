package com.smartpizza.core.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")  //table name 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {
	
	

    @Id  //primary key 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * User is managed by auth-service.
     * So in core-service, we store only userId instead of mapping User entity.
     */
    
    @Column(nullable = false, unique = true)  //unique = true, ensures one active cart per user.
    private Long userId;

    @Column(nullable = false)  //nullable = false means this column cannot contain a null value
    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /*
     * One cart can contain many cart items.(one to many relationship)
     * If cart is deleted, related cart items should also be deleted.
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)   //orphanRemoval = true removes cart items from the database
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    

     /*
     * @PrePersist (initialized before inserting a new row)Initializes default values before the cart is persisted.
     * This prevents null amount/list issues during cart creation.
     */

    
    @PrePersist  
    public void onCreate() {
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;   //if total amt is not set initailize it to 0
        }

        if (this.cartItems == null) {
            this.cartItems = new ArrayList<>();  // if the list of cart item is null, create an empty list
        }

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate  //initialized before updating a new row (update the updated at timestamp)
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
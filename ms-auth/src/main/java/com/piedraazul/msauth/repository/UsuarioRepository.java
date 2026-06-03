package com.piedraazul.msauth.repository;

import com.piedraazul.msauth.model.Usuario;
import com.piedraazul.msauth.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar usuario por username (para login)
    Optional<Usuario> findByUsername(String username);

    // Buscar usuarios por rol (HU-07, HU-08 — médicos y terapistas)
    List<Usuario> findByRol(Rol rol);

    // Verificar si un username ya existe
    boolean existsByUsername(String username);

    // Verificar si un email ya existe
    boolean existsByEmail(String email);
}
package com.piedraazul.msauth.service;

import com.piedraazul.msauth.model.Rol;
import com.piedraazul.msauth.model.Usuario;
import com.piedraazul.msauth.model.UsuarioFactory;
import com.piedraazul.msauth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // -- Crear usuario usando el Factory Method (GoF) --
    public Usuario crearUsuario(String username,
                                String password,
                                String nombre,
                                String email,
                                String rol,
                                Long pacienteId,
                                Long medicoId) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new RuntimeException("El username ya existe: " + username);
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("El email ya está registrado: " + email);
        }

        Usuario usuario = UsuarioFactory.crear(
                username,
                passwordEncoder.encode(password),
                nombre,
                email,
                rol,
                pacienteId,
                medicoId
        );
        return usuarioRepository.save(usuario);
    }

    // -- Login: valida credenciales --
    public Optional<Usuario> login(String username, String password) {
        return usuarioRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .filter(Usuario::isActivo);
    }

    // -- Listar usuarios por rol (HU-07, HU-08) --
    public List<Usuario> listarPorRol(String rol) {
        Rol rolEnum = Rol.valueOf(rol.toUpperCase());
        return usuarioRepository.findByRol(rolEnum);
    }

    // -- Listar todos los usuarios --
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // -- Desactivar usuario --
    public Usuario desactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        usuario.setActivo(false);
        return usuarioRepository.save(usuario);
    }
    // -- Buscar documento del paciente --
    public Optional<Usuario> buscarPorPacienteId(Long pacienteId) {
        return usuarioRepository.findByPacienteId(pacienteId);
    }

    // -- Verificar paciente --
    public boolean existePacienteConUsuario(Long pacienteId) {
        return usuarioRepository.existsByPacienteId(pacienteId);
    }
}
package com.piedraazul.msauth;

import com.piedraazul.msauth.model.Rol;
import com.piedraazul.msauth.model.Usuario;
import com.piedraazul.msauth.model.UsuarioFactory;
import com.piedraazul.msauth.repository.UsuarioRepository;
import com.piedraazul.msauth.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("password_encriptado");
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
    }

    // -- Test 1: Factory Method crea usuario correctamente --
    @Test
    void testFactoryMethod_CreaUsuarioAdministrador() {
        Usuario usuario = UsuarioFactory.crear(
                "admin1", "1234", "Administrador",
                "admin@piedraazul.com", "ADMINISTRADOR",
                null, null);

        assertNotNull(usuario);
        assertEquals(Rol.ADMINISTRADOR, usuario.getRol());
        assertEquals("admin1", usuario.getUsername());
        assertTrue(usuario.isActivo());
    }

    // -- Test 2: Rol inválido lanza excepción --
    @Test
    void testFactoryMethod_RolInvalido_LanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                UsuarioFactory.crear(
                        "user1", "1234", "Usuario",
                        "user@test.com", "ROL_INEXISTENTE",
                        null, null)
        );
    }

    // -- Test 3: Crear usuario guarda en repositorio --
    @Test
    void testCrearUsuario_GuardaEnRepositorio() {
        Usuario usuarioMock = new Usuario();
        usuarioMock.setUsername("admin1");
        usuarioMock.setRol(Rol.ADMINISTRADOR);
        usuarioMock.setActivo(true);

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioMock);

        Usuario resultado = usuarioService.crearUsuario(
                "admin1", "1234", "Administrador",
                "admin@piedraazul.com", "ADMINISTRADOR",
                null, null);

        assertNotNull(resultado);
        assertEquals("admin1", resultado.getUsername());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    // -- Test 4: Login fallido retorna Optional vacío --
    @Test
    void testLogin_CredencialesIncorrectas_RetornaVacio() {
        when(usuarioRepository.findByUsername("admin1"))
                .thenReturn(Optional.empty());

        Optional<Usuario> resultado = usuarioService.login("admin1", "1234");

        assertTrue(resultado.isEmpty());
    }
}
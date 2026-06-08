package com.piedraazul.mspacientes.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

// ══════════════════════════════════════════════════════
// PATRÓN GOF: CHAIN OF RESPONSIBILITY
// Propósito: interceptar cada petición entrante y
// verificar que el rol propagado por el API Gateway
// (header X-User-Role) coincida con los roles
// permitidos declarados en @RolRequerido.
//
// Flujo:
//  1. El API Gateway valida el JWT y extrae el rol
//  2. Lo propaga como header interno X-User-Role
//  3. Este interceptor lo lee y decide si autoriza
// ══════════════════════════════════════════════════════
@Component
public class RolInterceptor implements HandlerInterceptor {

    private static final String HEADER_ROL = "X-User-Role";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Solo aplica a métodos de controlador
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Buscar la anotación en el método; si no está, buscar en la clase
        RolRequerido anotacion = handlerMethod.getMethodAnnotation(RolRequerido.class);
        if (anotacion == null) {
            anotacion = handlerMethod.getBeanType().getAnnotation(RolRequerido.class);
        }

        // Si el endpoint no requiere rol específico, permitir acceso
        if (anotacion == null) {
            return true;
        }

        // Leer el rol propagado por el API Gateway
        String rolUsuario = request.getHeader(HEADER_ROL);

        if (rolUsuario == null || rolUsuario.isBlank()) {
            responderForbidden(response, "Acceso denegado: no se encontró rol de usuario.");
            return false;
        }

        // Verificar si el rol del usuario está entre los permitidos
        boolean autorizado = Arrays.asList(anotacion.value()).contains(rolUsuario);

        if (!autorizado) {
            responderForbidden(response,
                    "Acceso denegado: rol '" + rolUsuario
                    + "' no tiene permiso para este recurso.");
            return false;
        }

        return true;
    }

    private void responderForbidden(HttpServletResponse response, String mensaje)
            throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + mensaje + "\"}");
    }
}

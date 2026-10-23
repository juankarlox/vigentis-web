package com.vigentis.seguridad;

import com.vigentis.usuarios.Usuario;
import com.vigentis.usuarios.UsuarioRepository;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * En cada petición: lee el usuario del token y carga sus permisos ACTUALES desde la base.
 * Así, si un administrador cambia el rol o desactiva a alguien, aplica de inmediato.
 */
@Component
public class ConvertidorJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarios;

    public ConvertidorJwt(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new DisabledException("Token no válido");
        }
        Usuario u = usuarios.findById(id)
                .filter(Usuario::getActivo)
                .orElseThrow(() -> new DisabledException("El usuario no existe o está desactivado"));
        // El token debe ser de la misma empresa a la que pertenece el usuario
        Object empresa = jwt.getClaims().get("empresa");
        if (empresa == null || !String.valueOf(u.empresa()).equals(String.valueOf(empresa))) {
            throw new DisabledException("Token no válido para esta empresa");
        }
        List<GrantedAuthority> permisos = u.getRol().getPermisos().stream()
                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.getCodigo()))
                .toList();
        return new JwtAuthenticationToken(jwt, permisos, u.getNombre());
    }
}

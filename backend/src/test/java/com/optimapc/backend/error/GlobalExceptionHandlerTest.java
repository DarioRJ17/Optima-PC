package com.optimapc.backend.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI(uri);
        return req;
    }

    @Test
    void erroresDeValidacionSeMapeanPorCampoSinDuplicados() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "email", "El email no es valido"),
                new FieldError("obj", "email", "otro mensaje para el mismo campo"),
                new FieldError("obj", "nombre", "El nombre es obligatorio")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> respuesta = handler.handleValidationException(ex, request("/api/auth/register"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().fieldErrors())
                .containsEntry("email", "El email no es valido") // se queda el primero
                .containsKey("nombre");
    }

    @Test
    void responseStatusConflictoDeEmailSeMapeaComoErrorDeCampo() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email");
        ResponseEntity<ApiError> respuesta = handler.handleResponseStatusException(ex, request("/api/auth/register"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().fieldErrors()).containsKey("email");
    }

    @Test
    void responseStatusSinRazonUsaMensajePorDefecto() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);
        ResponseEntity<ApiError> respuesta = handler.handleResponseStatusException(ex, request("/api/pedidos/1"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody().message()).isEqualTo("Error en la solicitud");
        assertThat(respuesta.getBody().fieldErrors()).isNull();
    }

    @Test
    void excepcionGenericaDevuelve500() {
        ResponseEntity<ApiError> respuesta = handler.handleGenericException(
                new RuntimeException("boom"), request("/api/algo"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().message()).isEqualTo("Ha ocurrido un error inesperado");
    }
}

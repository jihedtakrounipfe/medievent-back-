package skylinkers.tn.mediconnectbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BiometricException extends RuntimeException {
    public BiometricException(String message) { super(message); }
}


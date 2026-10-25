package com.compi2.contacto.analisis;

import com.compi2.contacto.errores.Diagnostico;
import com.compi2.contacto.errores.Severidad;
import com.compi2.contacto.errores.TipoDiagnostico;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.nio.file.Path;
import java.util.List;

final class RecolectorErroresAntlr extends BaseErrorListener {

    private final Path archivo;
    private final TipoDiagnostico tipo;
    private final List<Diagnostico> diagnosticos;

    RecolectorErroresAntlr(
            Path archivo,
            TipoDiagnostico tipo,
            List<Diagnostico> diagnosticos
    ) {
        this.archivo = archivo;
        this.tipo = tipo;
        this.diagnosticos = diagnosticos;
    }

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object simboloIncorrecto,
            int linea,
            int columna,
            String mensaje,
            RecognitionException excepcion
    ) {
        diagnosticos.add(new Diagnostico(
                tipo,
                Severidad.ERROR,
                archivo,
                Math.max(linea, 1),
                Math.max(columna, 0),
                mensaje
        ));
    }
}


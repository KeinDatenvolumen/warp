// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-143] (2e513c7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: rest client configuration (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.security;

import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GraphQlOperationClassificationFilter
        extends OncePerRequestFilter {

    public static final String QUERY_REQUEST_ATTRIBUTE =
            GraphQlOperationClassificationFilter.class.getName()
                    + ".query";

    private final JsonMapper jsonMapper;

    public GraphQlOperationClassificationFilter(
            JsonMapper jsonMapper
    ) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !"/graphql".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        byte[] requestBody =
                request.getInputStream().readAllBytes();

        CachedBodyHttpServletRequest wrappedRequest =
                new CachedBodyHttpServletRequest(
                        request,
                        requestBody
                );

        wrappedRequest.setAttribute(
                QUERY_REQUEST_ATTRIBUTE,
                isGraphQlQuery(requestBody)
        );

        filterChain.doFilter(
                wrappedRequest,
                response
        );
    }

    private boolean isGraphQlQuery(byte[] requestBody) {
        try {
            JsonNode body = jsonMapper.readTree(requestBody);

            if (body == null || !body.isObject()) {
                return false;
            }

            JsonNode queryNode = body.get("query");

            if (queryNode == null
                    || !queryNode.isString()
                    || queryNode.stringValue().isBlank()) {
                return false;
            }

            String operationName = null;

            JsonNode operationNameNode =
                    body.get("operationName");

            if (operationNameNode != null
                    && operationNameNode.isString()
                    && !operationNameNode.stringValue().isBlank()) {
                operationName =
                        operationNameNode.stringValue();
            }

            Document document =
                    new Parser().parseDocument(
                            queryNode.stringValue()
                    );

            List<OperationDefinition> operations =
                    document.getDefinitionsOfType(
                            OperationDefinition.class
                    );

            OperationDefinition selectedOperation =
                    selectOperation(
                            operations,
                            operationName
                    );

            return selectedOperation != null
                    && selectedOperation.getOperation()
                    == OperationDefinition.Operation.QUERY;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private OperationDefinition selectOperation(
            List<OperationDefinition> operations,
            String operationName
    ) {
        if (operationName != null) {
            return operations.stream()
                    .filter(operation ->
                            operationName.equals(
                                    operation.getName()
                            )
                    )
                    .findFirst()
                    .orElse(null);
        }

        if (operations.size() == 1) {
            return operations.getFirst();
        }

        return null;
    }

    private static final class CachedBodyHttpServletRequest
            extends HttpServletRequestWrapper {

        private final byte[] requestBody;

        private CachedBodyHttpServletRequest(
                HttpServletRequest request,
                byte[] requestBody
        ) {
            super(request);
            this.requestBody = requestBody.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(requestBody);

            return new ServletInputStream() {

                @Override
                public int read() {
                    return inputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(
                        ReadListener readListener
                ) {
                    // Synchrone Verarbeitung.
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(
                    new InputStreamReader(
                            getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );
        }

        @Override
        public int getContentLength() {
            return requestBody.length;
        }

        @Override
        public long getContentLengthLong() {
            return requestBody.length;
        }
    }
}
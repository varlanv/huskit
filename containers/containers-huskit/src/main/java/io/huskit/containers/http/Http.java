package io.huskit.containers.http;

import java.util.Map;
import java.util.NoSuchElementException;

public interface Http {

    interface Request {

        byte[] body();

        static Request empty() {
            return () -> new byte[0];
        }
    }

    interface Response<T> {

        Head head();

        Body<T> body();

        static <T> Response<T> of(Head head, Body<T> body) {
            return new Response<>() {

                @Override
                public Head head() {
                    return head;
                }

                @Override
                public Body<T> body() {
                    return body;
                }
            };
        }
    }

    interface Head {

        Integer status();

        Map<String, String> headers();

        default Boolean isChunked() {
            return "chunked".equals(headers().get("Transfer-Encoding"));
        }

        default Boolean isMultiplexedStream() {
            return "application/vnd.docker.multiplexed-stream".equals(headers().get("Content-Type"));
        }
    }

    interface Body<T> {

        @SuppressWarnings("unchecked")
        static <T> Body<T> empty() {
            return EmptyBody.instance();
        }

        T value();

        static <T> Body<T> of(T value) {
            return new DfBody<>(value);
        }

        @SuppressWarnings("rawtypes")
        final class EmptyBody implements Body {

            private static final EmptyBody INSTANCE = new EmptyBody();

            private EmptyBody() {
            }

            private static EmptyBody instance() {
                return INSTANCE;
            }

            @Override
            public Object value() {
                throw new NoSuchElementException();
            }
        }
    }
}

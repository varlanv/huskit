package io.huskit.containers.http;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public interface Http {

    interface Request {

        byte[] body();

        static Request empty() {
            return () -> new byte[0];
        }
    }

    interface ResponseStream extends AutoCloseable {

        Head head();

        Reader reader();

        @Override
        default void close() throws Exception {
            reader().close();
        }
    }

    interface Response<T> {

        Head head();

        Body<T> body();
    }

    interface Head {

        Integer status();

        Map<String, String> headers();
    }

    interface StringBody extends Body<String> {
    }

    interface Body<T> {

        @SuppressWarnings("unchecked")
        static <T> Body<T> empty() {
            return EmptyBody.instance();
        }

        List<T> list();

        Stream<T> stream();

        T single();

        @SuppressWarnings("rawtypes")
        class EmptyBody implements Body {

            private static final EmptyBody INSTANCE = new EmptyBody();

            private EmptyBody() {
            }

            private static EmptyBody instance() {
                return INSTANCE;
            }

            @Override
            public List<?> list() {
                return List.of();
            }

            @Override
            public Stream<?> stream() {
                return Stream.empty();
            }

            @Override
            public Object single() {
                throw new NoSuchElementException();
            }
        }
    }
}

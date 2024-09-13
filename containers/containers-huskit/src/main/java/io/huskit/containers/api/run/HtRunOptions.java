package io.huskit.containers.api.run;

import java.util.Map;

public interface HtRunOptions {

    HtRunOptions withLabels(Map<String, String> labels);

    HtRunOptions withCommand(CharSequence command);

    HtRunOptions withRemove();

    Map<HtOptionType, HtOption> asMap();

    int size();
}

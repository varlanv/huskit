package io.huskit.containers.http;

import io.huskit.common.Mutable;
import io.huskit.common.collection.HtCollections;
import io.huskit.containers.api.container.list.HtListContainersFilterType;
import io.huskit.containers.api.container.list.arg.HtListContainersArgsSpec;
import io.huskit.containers.internal.HtJson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HtHttpListContainersSpec implements HtListContainersArgsSpec, HtUrl {

    Mutable<Boolean> all = Mutable.of(false);
    Map<HtListContainersFilterType, List<String>> filters = new HashMap<>();

    @Override
    public HtListContainersArgsSpec withAll() {
        this.all.set(true);
        return this;
    }

    @Override
    public HtListContainersArgsSpec withIdFilter(CharSequence id) {
        HtCollections.putOrAdd(filters, HtListContainersFilterType.ID, id.toString());
        return this;
    }

    @Override
    public HtListContainersArgsSpec withNameFilter(CharSequence name) {
        HtCollections.putOrAdd(filters, HtListContainersFilterType.NAME, name.toString());
        return this;
    }

    @Override
    public HtListContainersArgsSpec withLabelFilter(CharSequence label, CharSequence value) {
        HtCollections.putOrAdd(filters, HtListContainersFilterType.LABEL, label.toString() + "=" + value.toString());
        return this;
    }

    @Override
    public HtListContainersArgsSpec withLabelFilter(CharSequence label) {
        HtCollections.putOrAdd(filters, HtListContainersFilterType.LABEL, label.toString());
        return this;
    }

    public String toParameters() {
        var parameters = new ArrayList<String>();
        if (all.require()) {
            parameters.add("all=true");
        }
        if (!filters.isEmpty()) {
            var jsonObject = HtJson.toJson(
                    filters.entrySet().stream()
                            .collect(
                                    Collectors.toMap(
                                            entry -> entry.getKey().name().toLowerCase(),
                                            Map.Entry::getValue
                                    )
                            )
            );
            parameters.add("filters=" + jsonObject);
        }
        if (parameters.isEmpty()) {
            return "";
        } else {
            return "?" + String.join("&", parameters);
        }
    }

    @Override
    public String url() {
        return "/containers/json" + toParameters();
    }
}

package de.hammerhartes.andy.breakerbox.stream;

import com.google.common.collect.ImmutableList;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class Collectors {

    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toImmutableList() {
        final Supplier<ImmutableList.Builder<T>> supplier =
                ImmutableList::builder;
        final BiConsumer<ImmutableList.Builder<T>, T> accumulator =
                ImmutableList.Builder::add;
        final BinaryOperator<ImmutableList.Builder<T>> combiner =
                (t, u) -> t.addAll(u.build());
        final Function<ImmutableList.Builder<T>, ImmutableList<T>> finisher =
                ImmutableList.Builder::build;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    private Collectors() {
    }
}

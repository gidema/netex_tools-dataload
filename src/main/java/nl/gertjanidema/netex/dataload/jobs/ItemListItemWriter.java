package nl.gertjanidema.netex.dataload.jobs;

import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class ItemListItemWriter<T> implements ItemWriter<List<T>> {
    private final ItemWriter<T> wrappedItemWriter;

    public ItemListItemWriter(ItemWriter<T> wrappedItemWriter) {
        super();
        this.wrappedItemWriter = wrappedItemWriter;
    }

    @Override
    public void write(Chunk<? extends List<T>> chunk) throws Exception {
        Chunk<T> childChunk = new Chunk<>();
        chunk.getItems().forEach( items -> {
            items.forEach(item -> {
                childChunk.add(item);
            });
        });
        wrappedItemWriter.write(childChunk);
    }
}

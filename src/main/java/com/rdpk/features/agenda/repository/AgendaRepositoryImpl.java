package com.rdpk.features.agenda.repository;

import com.rdpk.features.agenda.domain.Agenda;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class AgendaRepositoryImpl implements AgendaRepository {

    private final R2dbcEntityTemplate template;

    public AgendaRepositoryImpl(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<Agenda> save(Agenda agenda) {
        if (agenda.id() == null) {
            return template.insert(Agenda.class)
                    .using(agenda);
        } else {
            return template.update(agenda);
        }
    }

    @Override
    public Mono<Agenda> findById(Long id) {
        return template.selectOne(
                Query.query(Criteria.where("id").is(id)),
                Agenda.class
        );
    }

    @Override
    public Flux<Agenda> findAll() {
        return template.select(Agenda.class).all();
    }
}

package com.com4energy.processor.repository;

import com.com4energy.persistence.clientes.BaseClienteRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ClienteRepository extends BaseClienteRepository {

    /**
     * Resuelve en UNA sola consulta todos los clientes cuyo CUPS empieza por alguno de
     * los prefijos de 20 caracteres dados (un único escaneo en vez de N lookups). Devuelve
     * el prefijo para reagrupar en memoria; varios clientes con el mismo prefijo = ambigüedad
     * (se trata como error aguas arriba).
     */
    @Query("""
            select c.id as id, c.tarifa as tarifa, substring(c.cups, 1, 20) as cupsPrefix
            from Cliente c
            where substring(c.cups, 1, 20) in :cupsPrefixes
            """)
    List<ClientePrefixView> findLookupByCupsPrefixes(@Param("cupsPrefixes") Collection<String> cupsPrefixes);

    interface ClientePrefixView {
        Integer getId();
        String getTarifa();
        String getCupsPrefix();
    }

}

package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variete;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarieteNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarieteRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class VarieteService {

    private final VarieteRepository varieteRepository;

    /**
     * Varieties Read Service
     * <p>Blank filter normalization method
     * <ul>
     *      <li>A filter sent as an empty or whitespace-only string means "no filter".</li>
     *      <li>It is turned into {@code null} so the query ignores it instead of
     *          looking for a variety whose block is literally the empty string.</li>
     * </ul>
     */
    private static String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Maps an entity to its API representation.
     *
     * <p>No value is derived or defaulted here: a NULL column stays a
     * {@code null} component, and the client decides how to display it.
     *
     * @param variete the entity to map
     * @return the API representation of that variety
     */
    private static VarietyResponse toResponse(Variete variete) {
        return new VarietyResponse(
                variete.getId(),
                variete.getIdFerme(),
                variete.getNom(),
                variete.getNombreArbres(),
                variete.getEspacementInterRangM(),
                variete.getEspacementIntraRangM(),
                variete.getDensiteArbresHa(),
                variete.getRendementAttenduKg(),
                variete.getRendementReelKg(),
                variete.getVigueur(),
                variete.getBlocParcelle(),
                variete.getOriginePlant(),
                variete.getSource(),
                variete.getDateMaj());
    }

    /**
     * Retrieves the varieties matching the optional filters.
     *
     * <p>Both filters are independent and optional. A filter that matches no
     * row is a normal outcome and returns an empty list; it is never an error.
     *
     * @param idFerme      farm identifier, or {@code null} for every farm
     * @param blocParcelle raw block value as stored (for example {@code "A"}),
     *                     or {@code null} for every block
     * @return the matching varieties, possibly empty
     */
    public List<VarietyResponse> obtainAllVarieties(Integer idFerme, String blocParcelle) {
        List<Variete> varietes = varieteRepository.findByOptionalFilters(
                idFerme,
                normalizeFilter(blocParcelle));
        return varietes.stream().map(VarieteService::toResponse).toList();
    }

    /**
     * Retrieves a single variety by its identifier.
     *
     * @param id the variety identifier
     * @return the representation of that variety
     * @throws VarieteNotFoundException if no variety exists with this ID
     */
    public VarietyResponse obtainVarietyById(Long id) {
        Variete variete = varieteRepository.findById(id)
                .orElseThrow(() -> new VarieteNotFoundException(id));
        return toResponse(variete);
    }
}

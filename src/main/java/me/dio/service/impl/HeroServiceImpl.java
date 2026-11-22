package me.dio.service.impl;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.dio.exception.BusinessException;
import me.dio.exception.NotFoundException;
import me.dio.model.Hero;
import me.dio.repository.HeroRepository;
import me.dio.service.HeroService;

@Service
@Transactional
public class HeroServiceImpl implements HeroService {

    /** XP granted per "up" vote (Double XP Event \o/). */
    static final int XP_PER_VOTE = 2;

    private final HeroRepository heroRepository;

    public HeroServiceImpl(HeroRepository heroRepository) {
        this.heroRepository = heroRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hero> findAll() {
        return this.heroRepository.findAll(Sort.by(Sort.Direction.DESC, "xp"));
    }

    @Override
    @Transactional(readOnly = true)
    public Hero findById(Long id) {
        return this.heroRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
    }

    @Override
    public Hero create(Hero heroToCreate) {
        // Never trust server-managed fields from the client: a non-null ID would
        // turn the insert into a merge that overwrites an existing hero.
        heroToCreate.setId(null);
        heroToCreate.setVersion(null);
        heroToCreate.setXp(0);
        return this.heroRepository.save(heroToCreate);
    }

    @Override
    public Hero update(Long id, Hero heroToUpdate) {
        Hero dbHero = this.findById(id);
        if (heroToUpdate.getId() != null && !dbHero.getId().equals(heroToUpdate.getId())) {
            throw new BusinessException("Update IDs must be the same.");
        }
        // Hibernate ignores version changes on managed entities, so compare explicitly.
        if (heroToUpdate.getVersion() != null && !heroToUpdate.getVersion().equals(dbHero.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Hero.class, id);
        }
        // Only "name" can be changed; "xp" is driven exclusively by votes.
        dbHero.setName(heroToUpdate.getName());
        return this.heroRepository.saveAndFlush(dbHero);
    }

    @Override
    public void delete(Long id) {
        Hero dbHero = this.findById(id);
        this.heroRepository.delete(dbHero);
    }

    @Override
    public void increaseXp(Long id) {
        Hero dbHero = this.findById(id);
        dbHero.setXp(dbHero.getXp() + XP_PER_VOTE);
        heroRepository.save(dbHero);
    }
}

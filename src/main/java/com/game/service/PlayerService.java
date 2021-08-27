package com.game.service;

import com.game.controller.PlayerOrder;
import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class PlayerService {

    private final PlayerRepository repo;

    @Autowired
    public PlayerService(PlayerRepository repo) {
        super();
        this.repo = repo;
    }

    public List<Player> getAllPlayers(String name, String title, Race race, Profession profession,
                                      Long after, Long before, Boolean banned, Integer minExperience,
                                      Integer maxExperience, Integer minLevel, Integer maxLevel) {
        final Date afterDate = after == null ? null : new Date(after);
        final Date beforeDate = before == null ? null : new Date(before);

        final List<Player> players = new ArrayList<>();
        repo.findAll().forEach(player -> {
            if (name != null && !player.getName().contains(name)) return;
            if (title != null && !player.getTitle().contains(title)) return;
            if (race != null && player.getRace() != race) return;
            if (profession != null && player.getProfession() != profession) return;
            if (after != null && player.getBirthday().before(afterDate)) return;
            if (before != null && player.getBirthday().after(beforeDate)) return;
            if (banned != null && player.isBanned().booleanValue() != banned.booleanValue()) return;
            if (minExperience != null && player.getExperience().compareTo(minExperience) < 0) return;
            if (maxExperience != null && player.getExperience().compareTo(maxExperience) > 0) return;
            if (minLevel != null && player.getLevel().compareTo(minLevel) < 0) return;
            if (maxLevel != null && player.getLevel().compareTo(maxLevel) > 0) return;
            players.add(player);
        });

        return players;
    }


    public List<Player> sortPlayers(List<Player> players, PlayerOrder order) {
        if (order != null) {
            players.sort((playerOne, playerTwo) -> {
                switch (order) {
                    case ID:
                        return Long.compare(playerOne.getId(), playerTwo.getId());
                    case NAME:
                        return playerOne.getName().compareTo(playerTwo.getName());
                    case LEVEL:
                        return Integer.compare(playerOne.getLevel(), playerTwo.getLevel());
                    case BIRTHDAY:
                        return playerOne.getBirthday().compareTo(playerTwo.getBirthday());
                    case EXPERIENCE:
                        return Integer.compare(playerOne.getExperience(), playerTwo.getExperience());
                    default:
                        return 0;
                }
            });
        }
        return players;
    }

    public Player createPlayer(Player player) {
        return repo.save(player);
    }

    public Player updatePlayer(Player oldPlayer, Player newPlayer) throws IllegalArgumentException {
        boolean isPossiblePlayerUpdate = false;

        final String name = newPlayer.getName();
        if (name != null) {
            if (isNameValid(name)) {
                oldPlayer.setName(name);
            } else {
                throw new IllegalArgumentException();
            }
        }

        final String title = newPlayer.getTitle();
        if (title != null) {
            if (isTitleValid(title)) {
                oldPlayer.setTitle(title);
            } else {
                throw new IllegalArgumentException();
            }
        }

        if (newPlayer.getRace() != null) {
            oldPlayer.setRace(newPlayer.getRace());
        }

        if (newPlayer.getProfession() != null) {
            oldPlayer.setProfession(newPlayer.getProfession());
        }

        final Integer experience = newPlayer.getExperience();
        if (experience != null) {
            if (isExperienceValid(experience)) {
                oldPlayer.setExperience(experience);
                isPossiblePlayerUpdate = true;
                oldPlayer.setLevel(calculateLevel(experience));
            } else {
                throw new IllegalArgumentException();
            }
        }

        Date birthday = newPlayer.getBirthday();
        if (birthday != null) {
            if (isBirthdayValid(birthday)) {
                oldPlayer.setBirthday(birthday);
                isPossiblePlayerUpdate = true;
            } else {
                throw new IllegalArgumentException();
            }
        }

        if (newPlayer.isBanned() != null) {
            oldPlayer.setBanned(newPlayer.isBanned());
            isPossiblePlayerUpdate = true;
        }

        if (isPossiblePlayerUpdate) {
            final int experienceUntilNextLevel =
                    calculateExperienceUntilNextLevel(oldPlayer.getLevel(), oldPlayer.getExperience());
            oldPlayer.setUntilNextLevel(experienceUntilNextLevel);
        }

        repo.save(oldPlayer);
        return oldPlayer;

    }

    public void deletePlayer(Player player) {
        repo.delete(player);
    }

    public List<Player> getPage(List<Player> players, Integer pageNumber, Integer pageSize) {
        final int page = pageNumber == null ? 0 : pageNumber;
        final int size = pageSize == null ? 3 : pageSize;

        final int from = page * size;

        int to = from + size;

        if (to > players.size()) {
            to = players.size();
        }

        return players.subList(from, to);
    }

    public boolean isValid(Player player) {
        return player != null && isNameValid(player.getName()) && isTitleValid(player.getTitle())
                && isExperienceValid(player.getExperience()) && isBirthdayValid(player.getBirthday());
    }


    public int calculateLevel(Integer experience) {
        return (int) (Math.sqrt(2500 + 200 * experience) - 50) / 100;
    }

    public int calculateExperienceUntilNextLevel(Integer level, Integer experience) {
        return 50 * (level + 1) * (level + 2) - experience;
    }

    public Player getPlayer(Long id) {
        return repo.findById(id).orElse(null);
    }

    private boolean isNameValid(String name) {
        final int maxNameLength = 12;
        return name != null && !name.isEmpty() && name.length() <= maxNameLength;
    }

    private boolean isTitleValid(String title) {
        final int maxTitleLength = 30;
        return title != null && !title.isEmpty() && title.length() <= maxTitleLength;
    }

    private boolean isExperienceValid(Integer exp) {
        final int minExp = 0;
        final int maxExp = 10_000_000;
        return exp != null && exp.compareTo(minExp) >= 0 && exp.compareTo(maxExp) <= 0;
    }

    private boolean isBirthdayValid(Date birthday) {
        final Date startYear = getDateFromYear(2000);
        final Date endYear = getDateFromYear(3000); //Дата регистрации (от 2000 до 3000 года включительно)
        return birthday != null && birthday.after(startYear) && birthday.before(endYear);
    }

    private Date getDateFromYear(int year) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        return calendar.getTime();
    }
}

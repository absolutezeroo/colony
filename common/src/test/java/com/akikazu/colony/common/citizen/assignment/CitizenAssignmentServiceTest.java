package com.akikazu.colony.common.citizen.assignment;

import com.akikazu.colony.api.building.room.RoomId;
import com.akikazu.colony.api.citizen.CitizenAssignmentService;
import com.akikazu.colony.api.citizen.CitizenId;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitizenAssignmentServiceTest
{
    private static final RegistryAccess EMPTY_LOOKUP = RegistryAccess.EMPTY;

    @Test
    void assignHomeRoomPersists()
    {
        CitizenAssignmentIndex index = new CitizenAssignmentIndex();
        CitizenAssignmentService service = CitizenAssignmentServiceImpl.wrapping(index);

        CitizenId citizen = CitizenId.random();
        RoomId room = RoomId.random();

        service.assignHomeRoom(citizen, room);

        CompoundTag saved = index.save(new CompoundTag(), EMPTY_LOOKUP);
        CitizenAssignmentIndex reloaded = CitizenAssignmentIndex.load(saved, EMPTY_LOOKUP);

        assertTrue(reloaded.roomOf(citizen).isPresent());
        assertEquals(room, reloaded.roomOf(citizen).orElseThrow());
    }

    @Test
    void citizensInRoomReturnsAssignedCitizens()
    {
        CitizenAssignmentIndex index = new CitizenAssignmentIndex();
        CitizenAssignmentService service = CitizenAssignmentServiceImpl.wrapping(index);

        RoomId room = RoomId.random();
        RoomId otherRoom = RoomId.random();

        CitizenId a = CitizenId.random();
        CitizenId b = CitizenId.random();
        CitizenId c = CitizenId.random();

        service.assignHomeRoom(a, room);
        service.assignHomeRoom(b, room);
        service.assignHomeRoom(c, otherRoom);

        Set<CitizenId> inRoom = service.citizensInRoom(room).collect(Collectors.toSet());

        assertEquals(Set.of(a, b), inRoom);
        assertEquals(Set.of(c), service.citizensInRoom(otherRoom).collect(Collectors.toSet()));
    }

    @Test
    void unassignClears()
    {
        CitizenAssignmentIndex index = new CitizenAssignmentIndex();
        CitizenAssignmentService service = CitizenAssignmentServiceImpl.wrapping(index);

        CitizenId citizen = CitizenId.random();
        RoomId room = RoomId.random();

        service.assignHomeRoom(citizen, room);
        assertTrue(index.roomOf(citizen).isPresent());

        service.unassignHomeRoom(citizen);

        assertFalse(index.roomOf(citizen).isPresent());
        assertEquals(0L, service.citizensInRoom(room).count());
    }
}

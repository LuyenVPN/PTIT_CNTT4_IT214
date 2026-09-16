package ra.inventoryservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventories")
public class InventoryController {
    @GetMapping
    public ResponseEntity<List<String>> getInventories(){
        return ResponseEntity.ok().body(List.of("Inventory 1", "Inventory 2", "Inventory 3"));
    }
}

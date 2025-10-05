package org.example.final_usth.api.controller;

import lombok.RequiredArgsConstructor;
import org.example.final_usth.api.model.PagedList;
import org.example.final_usth.api.model.ProductDto;
import org.example.final_usth.api.model.TradeDto;
import org.example.final_usth.marketdata.entity.Candle;
import org.example.final_usth.marketdata.entity.ProductEntity;
import org.example.final_usth.marketdata.entity.TradeEntity;
import org.example.final_usth.marketdata.repository.CandleRepository;
import org.example.final_usth.marketdata.repository.ProductRepository;
import org.example.final_usth.marketdata.repository.TradeRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Đây là controller phục vụ cho market data API – những thông tin thị trường mà frontend (hoặc client) cần hiển thị cho user:
 * Danh sách sản phẩm (/api/products) → hiển thị các cặp giao dịch (BTC-USD, ETH-USDT…).
 * Trade history (/api/products/{productId}/trades) → hiển thị các lệnh đã khớp gần nhất (dùng cho trade ticker).
 * Candles (OHLCV) (/api/products/{productId}/candles) → hiển thị biểu đồ nến trên chart.
 * Order Book (/api/products/{productId}/book) → hiển thị sổ lệnh theo nhiều cấp độ (L1, L2, L3).
 */
@RestController()
@RequiredArgsConstructor
public class ProductController {
    // Các dependency cần dùng (được Spring inject)
    private final OrderBookSnapshotManager orderBookSnapshotManager;  // Lấy snapshot orderbook (L1, L2, L3)
    private final ProductRepository productRepository;
    private final TradeRepository tradeRepository;
    private final CandleRepository candleRepository;

    @GetMapping("/api/products")
    public List<ProductDto> getProducts() {
        List<ProductEntity> products = productRepository.findAll();

        // Convert sang DTO để trả về client
        return products.stream().map(this::productDto).collect(Collectors.toList());
    }

    // -------------------- 2. Lấy danh sách trade gần nhất của 1 product --------------------
    @GetMapping("/api/products/{productId}/trades")
    public List<TradeDto> getProductTrades(@PathVariable String productId) {
        // Lấy 50 trade mới nhất của product
        List<TradeEntity> trades = tradeRepository.findByProductId(productId, 50);

        // Convert sang DTO để client hiển thị
        return trades.stream().map(this::tradeDto).collect(Collectors.toList());
    }

    // -------------------- 3. Lấy dữ liệu candles (OHLCV) cho chart --------------------
    @GetMapping("/api/products/{productId}/candles")
    public List<List<Object>> getProductCandles(@PathVariable String productId, @RequestParam int granularity,
                                                @RequestParam(defaultValue = "1000") int limit) {
        // Truy vấn candle từ DB (granularity/60 nghĩa là đơn vị phút)
        PagedList<Candle> candlePage = candleRepository.findAll(productId, granularity / 60, 1, limit);

        // Format dữ liệu về dạng mảng con (time, low, high, open, close, volume)
        //[ [time, low, high, open, close, volume], ... ]
        List<List<Object>> lines = new ArrayList<>();
        candlePage.getItems().forEach(x -> {
            List<Object> line = new ArrayList<>();
            line.add(x.getTime());
            line.add(x.getLow().stripTrailingZeros());
            line.add(x.getHigh().stripTrailingZeros());
            line.add(x.getOpen().stripTrailingZeros());
            line.add(x.getClose().stripTrailingZeros());
            line.add(x.getVolume().stripTrailingZeros());
            lines.add(line);
        });
        return lines;
    }

    // -------------------- 4. Lấy order book (sổ lệnh) --------------------
    @GetMapping("/api/products/{productId}/book")
    public Object getProductBook(@PathVariable String productId, @RequestParam(defaultValue = "2") int level) {
        // Tùy vào level, trả về độ sâu của orderbook:
        // L1: best bid/ask
        // L2: orderbook nhóm theo price level
        // L3: full orderbook chi tiết
        return switch (level) {
            case 1 -> orderBookSnapshotManager.getL1OrderBook(productId);
            case 2 -> orderBookSnapshotManager.getL2BatchOrderBook(productId);
            case 3 -> orderBookSnapshotManager.getL3OrderBook(productId);
            default -> null;
        };
    }
    private ProductDto productDto(ProductEntity product) {
        ProductDto productDto = new ProductDto();
        BeanUtils.copyProperties(product, productDto);              // Copy các field trùng tên
        productDto.setId(product.getId());                          // Set id thủ công
        productDto.setQuoteIncrement(String.valueOf(product.getQuoteIncrement())); // Format số về string
        return productDto;
    }

    private TradeDto tradeDto(TradeEntity trade) {
        TradeDto tradeDto = new TradeDto();
        tradeDto.setSequence(trade.getSequence());                   // sequence đảm bảo order của trade
        tradeDto.setTime(trade.getTime().toInstant().toString());    // format time sang ISO string
        tradeDto.setPrice(trade.getPrice().toPlainString());
        tradeDto.setSize(trade.getSize().toPlainString());
        tradeDto.setSide(trade.getSide().name().toLowerCase());      // BUY/SELL
        return tradeDto;
    }
}

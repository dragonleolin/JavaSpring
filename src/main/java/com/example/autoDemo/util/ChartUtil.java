package com.example.autoDemo.util;

import com.example.autoDemo.data.StockResponse;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class ChartUtil {

    public static byte[] generateLineChartImage(String code, List<StockResponse> stockList) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (StockResponse stock : stockList) {
            try {
                double price = Double.parseDouble(stock.getPrice());
                dataset.addValue(price, "價格", stock.getMarketTime());
            } catch (NumberFormatException ignored) {}
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "股票價格走勢 - " + code,
                "時間", "價格",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(out, chart, 800, 500);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成圖表失敗", e);
        }
    }

}

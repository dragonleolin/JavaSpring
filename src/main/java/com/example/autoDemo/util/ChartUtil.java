package com.example.autoDemo.util;

import com.example.autoDemo.data.StockResponse;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class ChartUtil {

    public static byte[] generateLineChartImage(String code, List<StockResponse> stockList) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;

        for (StockResponse stock : stockList) {
            try {
                double price = Double.parseDouble(stock.getPrice());
                dataset.addValue(price, "price", stock.getMarketTime());
                // 計算最小與最大價格
                if (price < minPrice) minPrice = price;
                if (price > maxPrice) maxPrice = price;
            } catch (NumberFormatException ignored) {}
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "股票價格走勢 - " + code,
                "time", "price",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.DARK_GRAY);
        plot.setRangeGridlinePaint(Color.DARK_GRAY);

        // Y 軸價格區間設定為 ±20 範圍
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        if (minPrice != Double.MAX_VALUE && maxPrice != Double.MIN_VALUE) {
            double lower = minPrice - 20;
            double upper = maxPrice + 20;
            rangeAxis.setRange(lower, upper);
        }

        // X 軸標籤轉向與顯示優化
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        // 折線與節點設定
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true); // 顯示節點
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
        renderer.setDefaultItemLabelsVisible(true); // 顯示標籤
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        plot.setRenderer(renderer);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(out, chart, 700, 400); // 適合手機寬度
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成圖表失敗", e);
        }
    }
}
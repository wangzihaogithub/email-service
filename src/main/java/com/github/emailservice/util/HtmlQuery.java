package com.github.emailservice.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.function.ToIntBiFunction;
import java.util.stream.Collectors;

/**
 * html查询工具
 * 1. 类似JQuery操作, 链式调用, 不用考虑过程中有null, 下标越界
 * 2. 扩展了jsoup不支持的功能 {@link #moveIndexByTagEq} {@link #prevByTagEq()} {@link #nextByTagEq()} {@link #breakParentByTagName(String)}
 *
 * @param <T>
 * @author hao
 */
public class HtmlQuery<T extends Element> {
    public static final HtmlQuery EMPTY = new HtmlQuery<>(null);
    private final T element;

    private HtmlQuery(T element) {
        this.element = element;
    }

    public static HtmlQuery<Document> valueOf(String html) {
        if (html == null || html.isEmpty()) {
            return EMPTY;
        }
        return new HtmlQuery<>(Jsoup.parse(html));
    }

    public static <T extends Element> HtmlQuery<T> valueOf(T element) {
        if (element == null) {
            return EMPTY;
        }
        return new HtmlQuery<>(element);
    }

    /**
     * css样式内联， 转行内样式
     *
     * @param html
     * @return 内联后
     */
    public static String inlineCss(String html) {
        final String style = "style";
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        Elements els = doc.select(style);// to get all the style elements
        for (Element e : els) {
            String styleRules = e.getAllElements().get(0).data().replaceAll("\n", "").trim();
            String delims = "{}";
            StringTokenizer st = new StringTokenizer(styleRules, delims);
            while (st.countTokens() > 1) {
                String selector = st.nextToken(), properties = st.nextToken();
                if (!selector.contains(":")) { // skip a:hover rules, etc.
                    Elements selectedElements = doc.select(selector);
                    for (Element selElem : selectedElements) {
                        String oldProperties = selElem.attr(style);
                        selElem.attr(style,
                                oldProperties.length() > 0 ? concatenateProperties(
                                        oldProperties, properties) : properties);
                    }
                }
            }
            e.remove();
        }
        return doc.toString();
    }

    private static String concatenateProperties(String oldProp, String newProp) {
        oldProp = oldProp.trim();
        if (!oldProp.endsWith(";")) {
            oldProp += ";";
        }
        return oldProp + newProp.replaceAll("\\s{2,}", "");
    }

    public T getElement() {
        return element;
    }

    public boolean isEmpty() {
        return element == null;
    }

    public HtmlQuery<Element> selectElement(String cssQuery, int index) {
        if (isEmpty()) {
            return EMPTY;
        }
        if (index < 0) {
            return EMPTY;
        } else if (index == 0) {
            return valueOf(element.selectFirst(cssQuery));
        } else {
            Elements elements = element.select(cssQuery);
            if (index >= elements.size()) {
                return EMPTY;
            } else {
                return valueOf(elements.get(index));
            }
        }
    }

    public List<String> selectAttrs(String cssQuery, String attr) {
        return selectElements(cssQuery).stream()
                .map(e -> e.attr(attr))
                .collect(Collectors.toList());
    }

    public String selectAttr(String cssQuery, String attr, int index) {
        HtmlQuery<Element> query = selectElement(cssQuery, index);
        return query.element != null ? query.element.attr(attr) : "";
    }

    public List<String> selectTexts(String cssQuery) {
        return selectElements(cssQuery).stream()
                .map(Element::text)
                .collect(Collectors.toList());
    }

    public String selectText(String cssQuery, int index) {
        if (isEmpty()) {
            return "";
        }
        Element element = selectElement(cssQuery, index).element;
        return element != null ? element.text() : "";
    }

    public String attr(String attributeKey) {
        if (isEmpty()) {
            return "";
        }
        return getElement().attr(attributeKey);
    }

    public String text() {
        if (isEmpty()) {
            return "";
        }
        return getElement().text();
    }

    /**
     * 同级相同元素移动下标
     *
     * @param choseIndexFn 选择下标方法
     * @return 返回选中的元素
     */
    public HtmlQuery<Element> moveIndexByTagEq(ToIntBiFunction<Elements, Element> choseIndexFn) {
        T element = getElement();
        if (isEmpty() || element.parent() == null) {
            return EMPTY;
        }
        String currentTagName = element.tagName();
        Elements elements = element.parent().children().stream()
                .filter(o -> Objects.equals(currentTagName, o.tagName()))
                .collect(Collectors.toCollection(Elements::new));
        int nextIndex = choseIndexFn.applyAsInt(elements, element);
        Element next = nextIndex >= 0 && nextIndex < elements.size() ? elements.get(nextIndex) : null;
        return valueOf(next);
    }

    public HtmlQuery<Element> prevByTagEq() {
        return moveIndexByTagEq(((elements, element) -> elements.indexOf(element) - 1));
    }

    public HtmlQuery<Element> nextByTagEq() {
        return moveIndexByTagEq(((elements, element) -> elements.indexOf(element) + 1));
    }

    public String selectTextValueContaining(String match, int index) {
        if (isEmpty()) {
            return "";
        }
        List<String> list = new ArrayList<>();
        for (Node node : getElement().childNodes()) {
            String text;
            if (node instanceof Element) {
                text = ((Element) node).text();
            } else {
                text = node.toString();
            }
            if (text.contains(match)) {
                list.add(text);
            }
        }
        return index >= 0 && index < list.size() ? list.get(index) : "";
    }

    public HtmlQuery<Element> breakParentByTagName(String tagName) {
        Element element = getElement();
        while (element != null && !Objects.equals(element.tagName(), tagName)) {
            element = element.parent();
        }
        return valueOf(element);
    }

    private Elements selectElements(String cssQuery) {
        if (isEmpty()) {
            return new Elements();
        }
        return getElement().select(cssQuery);
    }

    @Override
    public String toString() {
        return isEmpty() ? "empty" : element.toString();
    }

}
    
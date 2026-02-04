import 'package:bing_search_automation/query_type.dart';
import 'package:flutter/material.dart';

class ActionSelectionWidget extends StatefulWidget {
  final int? selectedIndex;
  final double topBarHeight;
  final void Function(int index, WidgetMenuItem?)? onSelected;

  const ActionSelectionWidget({
    super.key,
    this.selectedIndex,
    this.onSelected,
    this.topBarHeight = 80,
  });

  @override
  State<ActionSelectionWidget> createState() => _ExampleScreenState();
}

class _ExampleScreenState extends State<ActionSelectionWidget> {
  late int? _selectedIndex;

  final List<WidgetMenuItem> _items = [
    WidgetMenuItem(
      icon: Icons.search,
      type: QueryType.SEARCH,
      title: "Search",
      description: "Earn points by searching with Bing across web, mobile, and PC.",
    ),
    WidgetMenuItem(
      icon: Icons.menu_book,
      type: QueryType.READ,
      title: "Read",
      description: "Complete reading tasks, news articles, or daily sets for points.",
    ),
    WidgetMenuItem(
      icon: Icons.question_answer,
      type: QueryType.ANSWER,
      title: "Answer",
      description: "Take quizzes, polls, and trivia challenges to earn rewards.",
    ),
    WidgetMenuItem(
      icon: Icons.star,
      type: QueryType.OTHERS,
      title: "Others",
      description: "Earn through shopping, offers, streaks, and bonus activities.",
      enabled: false,
    ),
  ];


  @override
  void initState() {
    super.initState();
    _selectedIndex = widget.selectedIndex;
  }

  @override
  void didUpdateWidget(covariant ActionSelectionWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.selectedIndex != widget.selectedIndex) {
      _selectedIndex = widget.selectedIndex;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(0, widget.topBarHeight, 0, 16),
      child: GridView.builder(
        itemCount: _items.length,
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          childAspectRatio: 4 / 3,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
        ),
          itemBuilder: (context, index) {
            final item = _items[index];
            final isSelected = index == _selectedIndex;
            final isEnabled = item.enabled;

            return GestureDetector(
              onTap: isEnabled
                  ? () {
                setState(() {
                  _selectedIndex = isSelected ? null : index;
                });
                if (widget.onSelected != null) {
                  widget.onSelected!(
                      _selectedIndex ?? 0, isSelected ? null : item);
                }
              }
                  : null,
              child: Stack(
                children: [
                  Opacity(
                    opacity: isEnabled ? 1.0 : 0.5,
                    child: Container(
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: isSelected
                              ? Colors.blue.shade700
                              : Colors.grey.shade300,
                          width: 2,
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.05),
                            blurRadius: 8,
                            offset: Offset(2, 4),
                          )
                        ],
                      ),
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Icon(
                                item.icon,
                                size: 32,
                                color: isEnabled
                                    ? Colors.blue.shade700
                                    : Colors.grey,
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Text(
                                      item.title,
                                      style: TextStyle(
                                        fontSize: 14,
                                        fontWeight: FontWeight.bold,
                                        color: isEnabled
                                            ? Colors.black
                                            : Colors.grey,
                                      ),
                                    ),
                                  ],
                                ),
                              )
                            ],
                          ),
                          const SizedBox(height: 4),
                          Expanded(
                            child: Text(
                              item.description,
                              style: TextStyle(
                                fontSize: 12,
                                color: isEnabled
                                    ? Colors.black54
                                    : Colors.grey,
                              ),
                              maxLines: 3,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (isSelected && isEnabled)
                    Positioned(
                      top: 6,
                      right: 6,
                      child: CircleAvatar(
                        radius: 10,
                        backgroundColor: Colors.green,
                        child: const Icon(Icons.check,
                            color: Colors.white, size: 14),
                      ),
                    ),
                  if (!isEnabled)
                    Positioned.fill(
                      child: Container(
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.3),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: const Center(
                          child: Icon(Icons.lock, color: Colors.grey, size: 28),
                        ),
                      ),
                    ),
                ],
              ),
            );
          },
      ),
    );
  }
}

class WidgetMenuItem {
  final IconData icon;
  final String title;
  final String description;
  final Color background;
  final bool enabled;
  final QueryType type;

  const WidgetMenuItem({
    required this.icon,
    required this.title,
    required this.description,
    required this.type,
    this.background = Colors.white60,
    this.enabled = true,

  });
}

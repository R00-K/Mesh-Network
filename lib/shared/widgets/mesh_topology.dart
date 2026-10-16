import 'package:flutter/material.dart';

import '../../infra/api/mesh_dashboard_models.dart';

/// Interactive animated topology view for the current mesh snapshot.
class MeshTopology extends StatefulWidget {
  const MeshTopology({required this.nodes, this.onNodeTap, super.key});

  final List<MeshNode> nodes;
  final ValueChanged<MeshNode>? onNodeTap;

  @override
  State<MeshTopology> createState() => _MeshTopologyState();
}

class _MeshTopologyState extends State<MeshTopology>
    with SingleTickerProviderStateMixin {
  late final AnimationController _pulse = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 2),
  )
    ..repeat(reverse: true);

  @override
  void dispose() {
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _pulse,
      builder: (context, child) => LayoutBuilder(
        builder: (context, constraints) {
          return GestureDetector(
            onTapUp: (details) => _showNode(context, details.localPosition),
            child: CustomPaint(
              painter: _TopologyPainter(widget.nodes, _pulse.value),
              size: Size(constraints.maxWidth, constraints.maxHeight),
            ),
          );
        },
      ),
    );
  }

  void _showNode(BuildContext context, Offset point) {
    if (widget.nodes.isEmpty) return;
    final node = widget.nodes.reduce((a, b) {
      final da = (Offset(
                a.position.dx * context.size!.width,
                a.position.dy * context.size!.height,
              ) -
              point)
          .distance;
      final db = (Offset(
                b.position.dx * context.size!.width,
                b.position.dy * context.size!.height,
              ) -
              point)
          .distance;
      return da < db ? a : b;
    });
    if (widget.onNodeTap != null) {
      widget.onNodeTap!(node);
      return;
    }
    showModalBottomSheet<void>(
      context: context,
      builder: (_) => ListTile(
        leading: const Icon(Icons.router),
        title: Text(node.name),
        subtitle: Text('${node.battery}% battery • ${node.latency} ms latency'),
      ),
    );
  }
}

class _TopologyPainter extends CustomPainter {
  const _TopologyPainter(this.nodes, this.pulse);

  final List<MeshNode> nodes;
  final double pulse;

  @override
  void paint(Canvas canvas, Size size) {
    final points = {
      for (final node in nodes)
        node.id: Offset(node.position.dx * size.width, node.position.dy * size.height),
    };
    final linkPaint = Paint()
      ..color = const Color(0xff1f5270)
      ..strokeWidth = 1.5;
    for (var i = 1; i < nodes.length; i++) {
      canvas.drawLine(points['local']!, points[nodes[i].id]!, linkPaint);
    }
    if (nodes.length > 3) {
      canvas.drawLine(points[nodes[1].id]!, points[nodes[3].id]!, linkPaint);
    }
    for (final node in nodes) {
      final point = points[node.id]!;
      final color = switch (node.role) {
        MeshNodeRole.local => const Color(0xff42a5f5),
        MeshNodeRole.relay => const Color(0xff64ff8c),
        MeshNodeRole.destination => const Color(0xffffb74d),
        MeshNodeRole.peer => const Color(0xff39d98a),
      };
      canvas.drawCircle(point, 19 + (node.role == MeshNodeRole.local ? pulse * 4 : 0),
          Paint()..color = color.withAlpha(45));
      canvas.drawCircle(point, 10, Paint()..color = color);
      final textPainter = TextPainter(
        text: TextSpan(
          text: node.name,
          style: const TextStyle(color: Colors.white, fontSize: 10),
        ),
        textDirection: TextDirection.ltr,
      )..layout();
      textPainter.paint(canvas, point + const Offset(-16, 14));
    }
  }

  @override
  bool shouldRepaint(covariant _TopologyPainter oldDelegate) => true;
}

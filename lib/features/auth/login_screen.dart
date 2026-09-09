import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../infra/services/auth_state.dart';

/// Email/password entry point for the application.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;
  bool _isCreateAccount = false;

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final controller = ref.read(authControllerProvider.notifier);
    final success = _isCreateAccount
        ? await controller.createAccount(
            name: _nameController.text,
            phoneNumber: _phoneController.text,
            email: _emailController.text,
            password: _passwordController.text,
          )
        : await controller.signIn(email: _emailController.text, password: _passwordController.text);
    if (success && mounted) context.go('/');
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider);
    final colors = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      body: Stack(
        children: [
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    colors.surface,
                    Color.alphaBlend(colors.primary.withAlpha(18), colors.surface),
                    colors.surface,
                  ],
                ),
              ),
            ),
          ),
          SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 460),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Container(
                        alignment: Alignment.center,
                        child: Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: colors.primary.withAlpha(28),
                            shape: BoxShape.circle,
                            border: Border.all(color: colors.primary.withAlpha(90)),
                          ),
                          child: Icon(Icons.hub_outlined, size: 36, color: colors.primary),
                        ),
                      ),
                      const SizedBox(height: 18),
                      Text('Disaster Mesh', textAlign: TextAlign.center, style: textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w700)),
                      const SizedBox(height: 4),
                      Text('Disaster Mesh Network', textAlign: TextAlign.center, style: textTheme.labelLarge?.copyWith(color: colors.primary, letterSpacing: 1.8)),
                      const SizedBox(height: 10),
                      Text('Stay connected when infrastructure is unavailable.', textAlign: TextAlign.center, style: textTheme.bodyMedium),
                      const SizedBox(height: 24),
                      Card(
                        elevation: 0,
                        color: colors.surfaceContainerHighest.withAlpha(170),
                        child: Padding(
                          padding: const EdgeInsets.all(22),
                          child: Form(
                            key: _formKey,
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                Text(_isCreateAccount ? 'Create your account' : 'Welcome back', style: textTheme.titleLarge),
                                const SizedBox(height: 4),
                                Text(_isCreateAccount ? 'Create a secure identity for your rescue network.' : 'Sign in to access your mesh operations center.', style: textTheme.bodySmall),
                                const SizedBox(height: 22),
                                if (_isCreateAccount) ...[
                                  TextFormField(
                                    controller: _nameController,
                                    textCapitalization: TextCapitalization.words,
                                    textInputAction: TextInputAction.next,
                                    decoration: const InputDecoration(labelText: 'Full name', prefixIcon: Icon(Icons.person_outline), border: OutlineInputBorder()),
                                    validator: (value) => value == null || value.trim().length < 2 ? 'Enter your name' : null,
                                  ),
                                  const SizedBox(height: 14),
                                  TextFormField(
                                    controller: _phoneController,
                                    keyboardType: TextInputType.phone,
                                    textInputAction: TextInputAction.next,
                                    decoration: const InputDecoration(labelText: 'Phone number', prefixIcon: Icon(Icons.phone_outlined), border: OutlineInputBorder()),
                                    validator: (value) => value == null || value.trim().length < 7 ? 'Enter a valid phone number' : null,
                                  ),
                                  const SizedBox(height: 14),
                                ],
                                TextFormField(
                                  controller: _emailController,
                                  keyboardType: TextInputType.emailAddress,
                                  textInputAction: TextInputAction.next,
                                  autofillHints: const [AutofillHints.username],
                                  decoration: const InputDecoration(labelText: 'Email', prefixIcon: Icon(Icons.email_outlined), border: OutlineInputBorder()),
                                  validator: (value) {
                                    if (value == null || value.trim().isEmpty) return 'Enter your email';
                                    if (!value.contains('@')) return 'Enter a valid email';
                                    return null;
                                  },
                                ),
                                const SizedBox(height: 14),
                                TextFormField(
                                  controller: _passwordController,
                                  obscureText: _obscurePassword,
                                  textInputAction: TextInputAction.done,
                                  autofillHints: const [AutofillHints.password],
                                  onFieldSubmitted: (_) => _submit(),
                                  decoration: InputDecoration(
                                    labelText: 'Password',
                                    prefixIcon: const Icon(Icons.lock_outline),
                                    border: const OutlineInputBorder(),
                                    suffixIcon: IconButton(
                                      tooltip: _obscurePassword ? 'Show password' : 'Hide password',
                                      onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                                      icon: Icon(_obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                                    ),
                                  ),
                                  validator: (value) => value == null || value.length < 6 ? 'Use at least 6 characters' : null,
                                ),
                                if (authState.errorMessage != null) ...[
                                  const SizedBox(height: 12),
                                  Container(
                                    padding: const EdgeInsets.all(10),
                                    decoration: BoxDecoration(color: colors.errorContainer, borderRadius: BorderRadius.circular(10)),
                                    child: Text(authState.errorMessage!, style: TextStyle(color: colors.onErrorContainer)),
                                  ),
                                ],
                                const SizedBox(height: 22),
                                FilledButton(
                                  onPressed: authState.isLoading ? null : _submit,
                                  style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                                  child: authState.isLoading
                                      ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                                      : Text(_isCreateAccount ? 'Create account' : 'Sign in'),
                                ),
                                const SizedBox(height: 14),
                                TextButton(
                                  onPressed: authState.isLoading ? null : () => setState(() => _isCreateAccount = !_isCreateAccount),
                                  child: Text(_isCreateAccount ? 'Already have an account? Sign in' : 'New to the mesh? Create an account'),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 14),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                        decoration: BoxDecoration(color: colors.primary.withAlpha(16), borderRadius: BorderRadius.circular(12), border: Border.all(color: colors.primary.withAlpha(45))),
                        child: Row(
                          children: [
                            Icon(Icons.wifi_tethering, size: 18, color: colors.primary),
                            const SizedBox(width: 10),
                            Expanded(child: Text('Offline-ready mesh communications', style: textTheme.labelMedium)),
                            Icon(Icons.check_circle, size: 17, color: colors.primary),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text('Authentication will be connected to Firebase in the next step.', textAlign: TextAlign.center, style: textTheme.bodySmall),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

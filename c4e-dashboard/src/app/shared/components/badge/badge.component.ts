import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

export type BadgeVariant =
  | 'primary'
  | 'success'
  | 'danger'
  | 'warning'
  | 'secondary'
  | 'info'
  | 'purple';

export type BadgeSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'c4e-badge',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span [class]="badgeClass" [attr.aria-label]="ariaLabel || null">
      @if (icon) {
        <span class="c4e-badge__icon" aria-hidden="true">{{ icon }}</span>
      }
      <span class="c4e-badge__label"><ng-content /></span>
      @if (dot) {
        <span class="c4e-badge__dot" aria-hidden="true"></span>
      }
    </span>
  `,
  styles: [`
    :host {
      display: inline-flex;
    }

    .c4e-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      border-radius: 999px;
      font-weight: 600;
      line-height: 1.2;
      white-space: nowrap;
      border-width: 1px;
      border-style: solid;
      transition: box-shadow 0.15s ease;
    }

    /* ── Tamaños ── */
    .c4e-badge--sm {
      font-size: 0.68rem;
      padding: 0.16rem 0.5rem;
    }

    .c4e-badge--md {
      font-size: 0.78rem;
      padding: 0.24rem 0.65rem;
    }

    .c4e-badge--lg {
      font-size: 0.88rem;
      padding: 0.32rem 0.8rem;
    }

    /* ── Pill vs Rounded ── */
    .c4e-badge--pill {
      border-radius: 999px;
    }

    .c4e-badge--rounded {
      border-radius: 0.375rem;
    }

    /* ── Variantes pastel ── */
    .c4e-badge--primary {
      background: #eff6ff;
      border-color: #bfdbfe;
      color: #1d4ed8;
    }

    .c4e-badge--success {
      background: #f0fdf4;
      border-color: #bbf7d0;
      color: #15803d;
    }

    .c4e-badge--danger {
      background: #fef2f2;
      border-color: #fecaca;
      color: #b91c1c;
    }

    .c4e-badge--warning {
      background: #fffbeb;
      border-color: #fde68a;
      color: #92400e;
    }

    .c4e-badge--secondary {
      background: #f8fafc;
      border-color: #e2e8f0;
      color: #475569;
    }

    .c4e-badge--info {
      background: #f0f9ff;
      border-color: #bae6fd;
      color: #0369a1;
    }

    .c4e-badge--purple {
      background: #faf5ff;
      border-color: #e9d5ff;
      color: #7c3aed;
    }

    /* ── Dot indicador ── */
    .c4e-badge__dot {
      display: inline-block;
      width: 0.45rem;
      height: 0.45rem;
      border-radius: 50%;
      background: currentColor;
      opacity: 0.75;
    }

    /* ── Icono ── */
    .c4e-badge__icon {
      font-size: 0.85em;
      line-height: 1;
    }
  `]
})
export class BadgeComponent {
  /** Variante de color pastel */
  @Input() variant: BadgeVariant = 'secondary';

  /** Tamaño del badge */
  @Input() size: BadgeSize = 'md';

  /** Forma: pill (totalmente redondeado) o rounded (ligeramente redondeado) */
  @Input() shape: 'pill' | 'rounded' = 'pill';

  /** Emoji o carácter de icono que aparece a la izquierda del texto */
  @Input() icon?: string;

  /** Muestra un punto de estado a la derecha */
  @Input() dot = false;

  /** Etiqueta ARIA para accesibilidad */
  @Input() ariaLabel?: string;

  get badgeClass(): string {
    return [
      'c4e-badge',
      `c4e-badge--${this.variant}`,
      `c4e-badge--${this.size}`,
      `c4e-badge--${this.shape}`,
    ].join(' ');
  }
}


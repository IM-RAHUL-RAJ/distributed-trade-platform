import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/auth.service';
import { PreferenceRequest } from '../../core/models';

@Component({
  selector: 'app-preferences',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <h2>Customer Preferences</h2>
      <p class="muted">Help us tailor your experience. This quick questionnaire sets up your trading profile.</p>

      <div class="panel prefs" *ngIf="loaded">
        <div class="grid cols-2">
          <div class="field">
            <label>Trading experience</label>
            <select [(ngModel)]="form.tradingExperience">
              <option value="NONE">No prior experience</option>
              <option value="BEGINNER">Beginner</option>
              <option value="INTERMEDIATE">Intermediate</option>
              <option value="ADVANCED">Advanced</option>
              <option value="PROFESSIONAL">Professional</option>
            </select>
          </div>
          <div class="field">
            <label>Risk tolerance</label>
            <select [(ngModel)]="form.riskTolerance">
              <option value="CONSERVATIVE">Conservative</option>
              <option value="MODERATE">Moderate</option>
              <option value="AGGRESSIVE">Aggressive</option>
            </select>
          </div>
          <div class="field">
            <label>Trading style</label>
            <select [(ngModel)]="form.tradingStyle">
              <option value="DAY_TRADING">Day trading</option>
              <option value="SWING_TRADING">Swing trading</option>
              <option value="LONG_TERM">Long-term investing</option>
              <option value="MIXED">Mixed</option>
            </select>
          </div>
          <div class="field">
            <label>Investment horizon</label>
            <select [(ngModel)]="form.investmentHorizon">
              <option value="SHORT_TERM">Short term (&lt; 1 year)</option>
              <option value="MEDIUM_TERM">Medium term (1–3 years)</option>
              <option value="LONG_TERM">Long term (3+ years)</option>
            </select>
          </div>
          <div class="field">
            <label>Trading frequency</label>
            <select [(ngModel)]="form.tradingFrequency">
              <option value="DAILY">Daily</option>
              <option value="WEEKLY">A few times a week</option>
              <option value="MONTHLY">A few times a month</option>
              <option value="OCCASIONAL">Occasional</option>
            </select>
          </div>
          <div class="field">
            <label>Preferred sectors</label>
            <select multiple size="4" [(ngModel)]="form.preferredSectors">
              <option value="TECHNOLOGY">Technology</option>
              <option value="FINANCE">Finance</option>
              <option value="HEALTHCARE">Healthcare</option>
              <option value="ENERGY">Energy</option>
              <option value="CONSUMER">Consumer</option>
              <option value="INDUSTRIALS">Industrials</option>
            </select>
          </div>
        </div>

        <p *ngIf="error" class="error">{{ error }}</p>
        <div class="actions">
          <button class="btn primary" (click)="save()" [disabled]="saving">
            {{ saving ? 'Saving…' : 'Save Preferences' }}
          </button>
          <button class="btn" (click)="skip()" *ngIf="!requiresCompletion">Skip for now</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page { max-width: 860px; margin: 0 auto; }
    h2 { margin: 0 0 6px; font-size: 24px; }
    .prefs { padding: 24px; }
    .actions { margin-top: 20px; display: flex; gap: 12px; }
    .error { color: var(--sell); }
  `],
})
export class CustomerPreferencesComponent implements OnInit {
  loaded = false;
  saving = false;
  error = '';
  requiresCompletion = false;
  form: PreferenceRequest = {
    tradingExperience: 'BEGINNER',
    riskTolerance: 'MODERATE',
    tradingStyle: 'DAY_TRADING',
    investmentHorizon: 'MEDIUM_TERM',
    preferredSectors: ['TECHNOLOGY'],
    tradingFrequency: 'WEEKLY',
  };

  constructor(private api: ApiService, private router: Router) {}

  ngOnInit() {
    this.api.preferences.get().subscribe({
      next: (pref) => {
        if (pref.completed) {
          this.form = {
            tradingExperience: pref.tradingExperience,
            riskTolerance: pref.riskTolerance,
            tradingStyle: pref.tradingStyle,
            investmentHorizon: pref.investmentHorizon,
            preferredSectors: pref.preferredSectors,
            tradingFrequency: pref.tradingFrequency,
          };
        }
        this.loaded = true;
      },
      error: () => {
        this.requiresCompletion = true;
        this.loaded = true;
      },
    });
  }

  save() {
    if (!this.form.preferredSectors.length) {
      this.error = 'Select at least one preferred sector.';
      return;
    }
    this.saving = true;
    this.error = '';
    this.api.preferences.save(this.form).subscribe({
      next: () => {
        this.saving = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message || 'Failed to save preferences.';
      },
    });
  }

  skip() {
    this.router.navigate(['/dashboard']);
  }
}
import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { IStep } from '../step.model';

@Component({
  selector: 'jhi-step-detail',
  templateUrl: './step-detail.component.html',
  imports: [SharedModule, RouterModule],
})
export class StepDetailComponent {
  step = input<IStep | null>(null);

  previousState(): void {
    window.history.back();
  }
}

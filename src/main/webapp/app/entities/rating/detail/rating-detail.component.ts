import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { FormatMediumDatePipe } from 'app/shared/date';
import { IRating } from '../rating.model';

@Component({
  selector: 'jhi-rating-detail',
  templateUrl: './rating-detail.component.html',
  imports: [SharedModule, RouterModule, FormatMediumDatePipe],
})
export class RatingDetailComponent {
  rating = input<IRating | null>(null);

  previousState(): void {
    window.history.back();
  }
}

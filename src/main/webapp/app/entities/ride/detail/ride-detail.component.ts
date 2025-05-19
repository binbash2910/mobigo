import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { FormatMediumDatePipe } from 'app/shared/date';
import { IRide } from '../ride.model';

@Component({
  selector: 'jhi-ride-detail',
  templateUrl: './ride-detail.component.html',
  imports: [SharedModule, RouterModule, FormatMediumDatePipe],
})
export class RideDetailComponent {
  ride = input<IRide | null>(null);

  previousState(): void {
    window.history.back();
  }
}

import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { IGroup } from '../group.model';

@Component({
  selector: 'jhi-group-detail',
  templateUrl: './group-detail.component.html',
  imports: [SharedModule, RouterModule],
})
export class GroupDetailComponent {
  group = input<IGroup | null>(null);

  previousState(): void {
    window.history.back();
  }
}

import dayjs from 'dayjs/esm';
import { IBooking } from 'app/entities/booking/booking.model';
import { PaymentMethodEnum } from 'app/entities/enumerations/payment-method-enum.model';
import { PaymentStatusEnum } from 'app/entities/enumerations/payment-status-enum.model';

export interface IPayment {
  id: number;
  montant?: number | null;
  datePaiement?: dayjs.Dayjs | null;
  methode?: keyof typeof PaymentMethodEnum | null;
  statut?: keyof typeof PaymentStatusEnum | null;
  booking?: IBooking | null;
}

export type NewPayment = Omit<IPayment, 'id'> & { id: null };
